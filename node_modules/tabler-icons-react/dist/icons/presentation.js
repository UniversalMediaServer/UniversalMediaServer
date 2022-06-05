import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Presentation(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-presentation",
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
    y1: "4",
    x2: "21",
    y2: "4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 4v10a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-10"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "12",
    y1: "16",
    x2: "12",
    y2: "20"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "9",
    y1: "20",
    x2: "15",
    y2: "20"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 12l3 -3l2 2l3 -3"
  }));
}

export { Presentation as default };
