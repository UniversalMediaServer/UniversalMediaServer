import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Click(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-click",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "3",
    y1: "12",
    x2: "6",
    y2: "12"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "3",
    x2: "12",
    y2: "6"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7.8",
    y1: "7.8",
    x2: "5.6",
    y2: "5.6"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "16.2",
    y1: "7.8",
    x2: "18.4",
    y2: "5.6"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "7.8",
    y1: "16.2",
    x2: "5.6",
    y2: "18.4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 12l9 3l-4 2l-2 4l-3 -9"
  }));
}

export { Click as default };
