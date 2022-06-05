import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function Rollercoaster(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-rollercoaster",
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
    d: "M3 21a5.55 5.55 0 0 0 5.265 -3.795l.735 -2.205a8.775 8.775 0 0 1 8.325 -6h3.675"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M20 9v12"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 21v-3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 21v-10"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 9.5v11.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15 3h5v3h-5z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 8l4 -3l2 2.5l-4 3l-1.8 -.5z"
  }));
}

export { Rollercoaster as default };
