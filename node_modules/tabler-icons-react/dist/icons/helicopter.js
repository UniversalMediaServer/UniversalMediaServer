import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Helicopter(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-helicopter",
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
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 10l1 2h6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 9a2 2 0 0 0 -2 2v3c0 1.1 .9 2 2 2h7a2 2 0 0 0 2 -2c0 -3.31 -3.13 -5 -7 -5h-2z"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "13",
    y1: "9",
    x2: "13",
    y2: "6"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "5",
    y1: "6",
    x2: "20",
    y2: "6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 9.1v3.9h5.5"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "15",
    y1: "19",
    x2: "15",
    y2: "16"
  }), /*#__PURE__*/React.createElement("line", {
    x1: "19",
    y1: "19",
    x2: "11",
    y2: "19"
  }));
}

export { Helicopter as default };
